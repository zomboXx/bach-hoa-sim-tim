// The training world owns no operational product, invoice or inventory records.
export type Point = { x: number; y: number };
export type Station = {
  id: string;
  name: string;
  x: number;
  y: number;
  w: number;
  h: number;
  type: string;
};
export const COLS = 20;
export const ROWS = 14;
export const stations: Station[] = [
  { id: "mentor", name: "Mentor Mai", x: 3, y: 3, w: 1, h: 1, type: "mentor" },
  {
    id: "customer",
    name: "Khách Linh",
    x: 15,
    y: 10,
    w: 1,
    h: 1,
    type: "customer",
  },
  {
    id: "milk",
    name: "Kệ sữa & đồ uống",
    x: 7,
    y: 3,
    w: 3,
    h: 2,
    type: "shelf",
  },
  { id: "snacks", name: "Kệ bánh kẹo", x: 7, y: 7, w: 3, h: 2, type: "shelf" },
  { id: "cooler", name: "Tủ mát", x: 14, y: 2, w: 4, h: 2, type: "cooler" },
  {
    id: "checkout",
    name: "Quầy thanh toán",
    x: 14,
    y: 6,
    w: 4,
    h: 2,
    type: "checkout",
  },
  {
    id: "quarantine",
    name: "Khu hàng cần xử lý",
    x: 2,
    y: 7,
    w: 3,
    h: 2,
    type: "quarantine",
  },
];
export const blocked = (p: Point) =>
  p.x < 1 ||
  p.y < 2 ||
  p.x >= COLS - 1 ||
  p.y >= ROWS - 1 ||
  stations.some(
    (s) => p.x >= s.x && p.x < s.x + s.w && p.y >= s.y && p.y < s.y + s.h,
  );
export function adjacent(p: Point, s: Station) {
  return (
    Math.abs(p.x - Math.max(s.x, Math.min(p.x, s.x + s.w - 1))) +
      Math.abs(p.y - Math.max(s.y, Math.min(p.y, s.y + s.h - 1))) ===
    1
  );
}
const directions = [
  { x: 0, y: 1 },
  { x: 0, y: -1 },
  { x: 1, y: 0 },
  { x: -1, y: 0 },
];
export function pathTo(
  start: Point,
  goal: (p: Point) => boolean,
): Point[] | null {
  const queue: { p: Point; path: Point[] }[] = [{ p: { ...start }, path: [] }];
  const seen = new Set([`${start.x},${start.y}`]);
  for (let i = 0; i < queue.length; i++) {
    const { p, path } = queue[i];
    if (goal(p)) return path;
    for (const d of directions) {
      const next = { x: p.x + d.x, y: p.y + d.y };
      const key = `${next.x},${next.y}`;
      if (blocked(next) || seen.has(key)) continue;
      seen.add(key);
      queue.push({ p: next, path: [...path, next] });
    }
  }
  return null;
}
