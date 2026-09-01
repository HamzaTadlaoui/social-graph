# Third-party assets

The only third-party assets in this repository. Both are licensed under the
SIL Open Font License 1.1, which is compatible with the app's GPL-3.0-or-later.

| File | Face | Project | Licence |
|---|---|---|---|
| `ibm_plex_mono_regular.ttf` | IBM Plex Mono Regular | [IBM Plex](https://github.com/IBM/plex) | OFL-1.1 |
| `ibm_plex_mono_medium.ttf` | IBM Plex Mono Medium | [IBM Plex](https://github.com/IBM/plex) | OFL-1.1 |
| `barlow_condensed_medium.ttf` | Barlow Condensed Medium | [Barlow](https://github.com/jpt/barlow) | OFL-1.1 |
| `barlow_condensed_semibold.ttf` | Barlow Condensed SemiBold | [Barlow](https://github.com/jpt/barlow) | OFL-1.1 |

Retrieved from the Google Fonts repository at `github.com/google/fonts`, under
`ofl/ibmplexmono/` and `ofl/barlowcondensed/`.

They are bundled rather than fetched through the downloadable-fonts provider on
purpose: the app has no `INTERNET` permission, and it should not gain one to render
a heading. See `app/src/main/java/io/github/hamzatadlaoui/socialgraph/ui/theme/Type.kt`
for how they are used.
