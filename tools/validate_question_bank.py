#!/usr/bin/env python3
"""Offline structural/content audit. Does not certify factual accuracy."""
import collections, json, pathlib, unicodedata
root=pathlib.Path(__file__).resolve().parents[1]
bank=json.loads((root/'app/src/main/assets/questions.json').read_text())
categories='General Science History Geography Technology Sports Movies Music Literature Animals Food Space'.split()
levels='Easy Normal Hard Expert'.split()
normalize=lambda text: ' '.join(unicodedata.normalize('NFKC',text).casefold().split())
assert len(bank)==240
assert len({q['id'] for q in bank})==len(bank)
assert len({normalize(q['text']) for q in bank})==len(bank)
counts=collections.Counter()
for q in bank:
    assert q['category'] in categories and q['difficulty'] in levels
    assert len(q['options'])==4 and len({normalize(x) for x in q['options']})==4
    assert all(x.strip() for x in q['options']) and 0<=q['correct']<=3
    assert q['source'].startswith('https://') and q['explanation'].strip() and q['reviewed']
    counts[q['category'],q['difficulty']]+=1
assert all(counts[c,d]==5 for c in categories for d in levels)
print('PASS: 240 stable unique IDs/prompts; four distinct answers each; 12 categories x 4 difficulties x 5 questions.')
print('All Categories: 60 per difficulty. Category: 20 total, 5 per difficulty.')
print('Facts require source review; structural validation is not a truth check.')
