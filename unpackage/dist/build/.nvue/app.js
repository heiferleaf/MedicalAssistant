Promise.resolve("./pages/test/health.js").then((res) => {
  res();
});
Promise.resolve("./pages/test/camera.js").then((res) => {
  res();
});
Promise.resolve("./app.css.js").then(() => {
});
