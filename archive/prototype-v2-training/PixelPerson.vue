<script setup lang="ts">
withDefaults(
  defineProps<{ kind?: string; walking?: boolean; direction?: string }>(),
  { kind: "player", walking: false, direction: "down" },
);
</script>
<template>
  <g
    :class="['pixel-sprite', kind, { walking }]"
    :style="{
      '--shirt': kind === 'customer' ? '#63958b' : '#78569a',
      '--hair':
        kind === 'mentor'
          ? '#d48748'
          : kind === 'customer'
            ? '#795043'
            : '#443345',
    }"
  >
    <ellipse cx="20" cy="35" rx="15" ry="5" fill="#413648" opacity=".2" />
    <g
      class="sprite-body"
      :transform="
        direction === 'left' ? 'translate(40 0) scale(-1 1)' : undefined
      "
    >
      <path
        class="sprite-leg leg-left"
        d="M10 24h8v12H8v-5h2z"
        fill="#393346"
      />
      <path
        class="sprite-leg leg-right"
        d="M22 24h8v12H20v-5h2z"
        fill="#393346"
      />
      <path d="M7 6h26v23H7z" fill="var(--shirt)" /><path
        d="M9 6h22v6H9z"
        fill="#ae8dbc"
      />
      <path d="M3 10h6v15H3zm28 0h6v15h-6z" fill="#efb990" />
      <path d="M10-16h21v22H10z" fill="#f3cba3" />
      <path d="M7-16h26v10H7zm3-5h20v6H10zM7-10h5V0H7z" fill="var(--hair)" />
      <template v-if="direction !== 'up'"
        ><path d="M15-4h3v4h-3zm10 0h3v4h-3z" fill="#473645" /><path
          d="M19 3h4v2h-4z"
          fill="#bf846b"
      /></template>
      <path v-else d="M10-10h21V4H10z" fill="var(--hair)" />
      <path
        v-if="kind === 'mentor' && direction !== 'up'"
        d="M12-6h8v7h-8zm11 0h8v7h-8zm-3 2h3"
        stroke="#593951"
        stroke-width="2"
        fill="none"
      />
      <path v-if="kind === 'player'" d="M23 14h7v5h-7z" fill="#ffdda0" />
    </g>
  </g>
</template>
