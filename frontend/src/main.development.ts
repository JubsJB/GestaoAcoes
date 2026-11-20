// This entry is selected only by the development build configuration.
// The sample has its own bootstrap: no session, API providers or application shell.
if (window.location.pathname === '/__dev/amostra-a') {
  Promise.all([
    import('@angular/platform-browser'),
    import('./app/development/visual-sample/visual-sample.component')
  ]).then(([{ bootstrapApplication }, { VisualSampleComponent }]) =>
    bootstrapApplication(VisualSampleComponent)
  ).catch(error => console.error(error));
} else {
  import('./main').catch(error => console.error(error));
}
