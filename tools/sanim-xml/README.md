# Sanim XML translator

Open `src/main/frontend/public/sanim-xml-translator.html` directly in a browser,
or visit `/sanim-xml-translator.html` on the running frontend. No server,
network requests, or installation is required for the standalone file.

Select one of the five XML types, paste XML or open a file, and click the
conversion button. Copy or download the resulting UTF-8 XML. The table shows
exact source titles and values; XML names use underscores for invalid name
characters, and duplicate names include the original tag for disambiguation.
Unknown and explicitly undefined fields retain their original names.

The source has 315 rows (307 distinct type/tag pairs). In LTR_EF23 and
LTR_EF23_1, PD/QD/RD/SD have conflicting mappings for goods/services versus
waste fees. Choose the applicable section; these tags remain unchanged until
a section is selected. All source alternatives are embedded, without inference.
Only element names are translated; attributes and values are preserved.
XML formatting and entity spellings may be normalized by browser serialization.
DTD input is unsupported. Data is not stored or sent anywhere.

Rebuild after editing the template or updating the source spreadsheet:

```sh
python3 tools/sanim-xml/build.py /path/to/all_sanim_fields.ods
```

`browser-tests.js` is a browser regression suite. Insert it as an additional
script after the application script in a temporary copy of the generated HTML.
Open the copy in a browser; `#test-results` reports checks for every mapping,
ambiguous fields, XML validity, namespaces, repeated elements, CDATA, comments,
unknown fields, text escaping, and stale-output invalidation.
