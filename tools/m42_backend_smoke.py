#!/usr/bin/env python3
"""Reviewed post-deployment checks: HTTPS GETs only, no generation or provisioning.

DO NOT run during preparation: room reads update M4 rate/presence bookkeeping.
"""
import json
from pathlib import Path
import ssl
import sys
import urllib.request
import urllib.error


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        raise RuntimeError('REDIRECT_REJECTED')


def request(opener, url, token=None):
    headers = {'Authorization': 'Bearer ' + token} if token else {}
    req = urllib.request.Request(url, headers=headers)
    try:
        with opener.open(req, timeout=10) as response:
            raw = response.read(32769)
            if len(raw) > 32768:
                raise RuntimeError()
            return response.status, json.loads(raw)
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read(4096))


if __name__ == '__main__':
    try:
        context = json.loads(Path(sys.argv[1]).read_text())
        origin = context['origin']
        if not origin.startswith('https://') or not origin.endswith('.trycloudflare.com'):
            raise RuntimeError()
        opener = urllib.request.build_opener(NoRedirect(), urllib.request.HTTPSHandler(context=ssl.create_default_context()))
        # No bearer credential is ever sent to loopback HTTP.
        plain = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
        code, body = request(plain, 'http://127.0.0.1:8787/health')
        result = {'productionRejectsPlainHttp': code == 400 and body.get('error', {}).get('code') == 'HTTPS_REQUIRED'}
        code, body = request(opener, origin + '/health')
        result['publicHttpsHealth'] = code == 200 and body == {'status': 'ok'}
        code, body = request(opener, origin + '/api/rooms/current')
        result['publicHttpsRejectsMissingToken'] = code == 401 and body.get('error', {}).get('code') == 'AUTH_REQUIRED'
        for i, token_file in enumerate(context['tokenFiles'], 1):
            token = Path(token_file).read_text().strip()
            if not 43 <= len(token) <= 128:
                raise RuntimeError()
            code, body = request(opener, origin + '/api/rooms/current', token)
            result['existingIdentity' + str(i)] = code == 200 and body.get('protocolVersion') == 1 and body.get('authority', {}).get('network') == 'ONLINE'
        print(json.dumps(result, indent=2))
        if not all(result.values()):
            raise SystemExit(1)
    except Exception:
        print('HTTPS smoke check failed; credentials and response contents omitted. Use reviewed source rollback if needed.')
        raise SystemExit(1)
