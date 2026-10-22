<script setup lang="ts">
import { computed, ref } from "vue";

const props = defineProps<{
  sku: string;
  name: string;
  emoji?: string;
  /** URL ảnh đại diện do người dùng upload; ưu tiên hơn ảnh tĩnh mặc định. */
  imageUrl?: string | null;
}>();

// Illustrations for the bundled sample catalog; unknown products keep their own placeholder.
const photos: Record<string, string> = {
  "ST-RICE-01": "rice",
  "ST-APPLE-01": "apple",
  "ST-MILK-01": "milk",
  "ST-COOKIE-01": "cookies",
  "ST-NOODLE-01": "noodles",
  "ST-OIL-01": "oil",
  "ST-SUGAR-01": "sugar",
  "ST-SALT-01": "salt",
  "ST-WATER-01": "water",
  "ST-COFFEE-01": "coffee",
  SP001: "milk",
  SP002: "cookies",
  SP004: "noodles",
  SP007: "rice",
};

const source = computed(() => {
  if (props.imageUrl) return props.imageUrl;
  const photo = photos[props.sku];
  return photo ? `/product-images/${photo}.webp` : undefined;
});
const failedSource = ref<string>();
</script>

<template>
  <img
    v-if="source && failedSource !== source"
    class="product-image"
    :src="source"
    :alt="name"
    width="480"
    height="480"
    loading="lazy"
    decoding="async"
    @error="failedSource = source"
  />
  <span v-else class="product-image-fallback" aria-hidden="true">{{ emoji || "📦" }}</span>
</template>

<style scoped>
.product-image {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: inherit;
  background: #f6f2ed;
}
.product-image-fallback {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  font-size: inherit;
}
</style>
