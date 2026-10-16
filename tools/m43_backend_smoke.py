#!/usr/bin/env python3
"""TLS health-only is read-only. Full smoke is for the approved post-deployment window.

Full room GETs update normal server rate/presence bookkeeping. Never log responses.
"""
import argparse
import json
import ssl
import urllib.request
import urllib.error
from urllib.parse import urlsplit
from pathlib import Path
from m43_backend_ops import private, read, require, verify_release, LIVE


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        raise RuntimeError('REDIRECT_REFUSED')


def request(opener, url, token=None, classic=False):
    headers = {}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if classic:
        headers['X-QuizPK-Features'] = 'classic-v1'
    try:
        response = opener.open(urllib.request.Request(url, headers=headers), timeout=15)
    except urllib.error.HTTPError as e:
        response = e
    with response as r:
        raw = r.read(32769)
        require(len(raw) <= 32768)
        return r.code, json.loads(raw)


def smoke(prep, health_only=False, fallback=False):
    _, context = verify_release(private(prep))
    origin = context['origin']; u = urlsplit(origin)
    require(u.scheme == 'https' and u.hostname and not u.username and not u.password
            and not u.query and not u.fragment and u.path in ('', '/'))
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect(),
        urllib.request.HTTPSHandler(context=ssl.create_default_context()))
    plain = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
    code, body = request(plain, 'http://127.0.0.1:8787/health')
    result = {'plainLoopbackRejected': code == 400 and body.get('error', {}).get('code') == 'HTTPS_REQUIRED'}
    code, body = request(opener, origin.rstrip('/') + '/health')
    result['publicTlsHealth'] = code == 200 and body == {'status': 'ok'}
    if not health_only:
        path = origin.rstrip('/') + '/api/rooms/current'
        code, body = request(opener, path)
        result['missingTokenRejected'] = code == 401 and body.get('error', {}).get('code') == 'AUTH_REQUIRED'
        for i, p in enumerate(sorted((LIVE / 'private').glob('*.token')), 1):
            token = p.read_text().strip()
            code, body = request(opener, path, token, True)
            result['currentToken' + str(i)] = code == 200 and body.get('protocolVersion') == 1 and body.get('authority', {}).get('network') == 'ONLINE'
            room = body.get('room')
            if room and room.get('state') == 'LOBBY':
                expected = body['room']['mode'] == 'CLASSIC' and not fallback
                result['lobbyCapability' + str(i)] = body.get('capabilities', {}).get('matches') is expected
        code, _ = request(opener, path, Path(context['revokedTokenFile']).read_text().strip(), True)
        result['exposedTokenRejected'] = code == 401
    require(all(result.values()))
    return result


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('preparation_directory')
    p.add_argument('--health-only', action='store_true')
    p.add_argument('--fallback', action='store_true')
    a = p.parse_args()
    try:
        print(json.dumps(smoke(Path(a.preparation_directory), a.health_only, a.fallback), indent=2))
    except Exception:
        print('TLS smoke failed; URL, credentials and response contents omitted. Keep review gate closed.')
        raise SystemExit(1)
