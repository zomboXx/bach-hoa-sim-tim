import type { Page } from "@playwright/test";

/** Replace only camera hardware: the real decoder still reads actual barcode pixels. */
export async function cameraFromImage(page: Page, imagePath: string) {
  await page.addInitScript((source) => {
    Object.defineProperty(navigator.mediaDevices, "getUserMedia", {
      configurable: true,
      value: async () => {
        const image = new Image();
        image.src = source;
        await image.decode();
        const canvas = document.createElement("canvas");
        canvas.width = image.naturalWidth;
        canvas.height = image.naturalHeight;
        const ctx = canvas.getContext("2d")!;
        ctx.drawImage(image, 0, 0);
        const stream = canvas.captureStream(10);
        const redraw = setInterval(() => ctx.drawImage(image, 0, 0), 100);
        stream.getVideoTracks()[0].addEventListener("ended", () => clearInterval(redraw));
        (window as Window & { posTestStream?: MediaStream }).posTestStream = stream;
        return stream;
      },
    });
  }, imagePath);
}
