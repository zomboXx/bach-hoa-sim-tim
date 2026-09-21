const CACHE = "simtim-prototype-v2-shell-sprint1";
self.addEventListener("install", (event) => {
  event.waitUntil(
    (async () => {
      const cache = await caches.open(CACHE);
      await cache.addAll([
        "/",
        "/index.html",
        "/icon.svg",
        "/manifest.webmanifest",
        "/mentor.png",
      ]);
      const html = await (await cache.match("/index.html")).text();
      const assets = [
        ...html.matchAll(/(?:src|href)="(\/assets\/[^\"]+)"/g),
      ].map((match) => match[1]);
      await cache.addAll(assets);
      await self.skipWaiting();
    })(),
  );
});
self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter(
              (k) => k.startsWith("simtim-prototype-v2-shell-") && k !== CACHE,
            )
            .map((k) => caches.delete(k)),
        ),
      )
      .then(() => self.clients.claim()),
  );
});
self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  const isPublicShell =
    [
      "/",
      "/index.html",
      "/icon.svg",
      "/manifest.webmanifest",
      "/mentor.png",
    ].includes(url.pathname) || url.pathname.startsWith("/assets/");
  if (
    event.request.method !== "GET" ||
    url.origin !== self.location.origin ||
    !isPublicShell
  )
    return;
  event.respondWith(
    fetch(event.request)
      .then((response) => {
        if (response.ok) {
          const copy = response.clone();
          event.waitUntil(
            caches.open(CACHE).then((c) => c.put(event.request, copy)),
          );
        }
        return response;
        // Vite adds Vary: Origin. These public, same-origin static assets have one
        // representation, whether precached without Origin or requested as a module.
      })
      .catch(
        async () =>
          (await caches.match(event.request, { ignoreVary: true })) ||
          (event.request.mode === "navigate"
            ? await caches.match("/index.html", { ignoreVary: true })
            : Response.error()),
      ),
  );
});
