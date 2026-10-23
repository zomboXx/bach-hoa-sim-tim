<script setup lang="ts">
import { ref, watch } from "vue";
const props = defineProps<{
  quantity: number;
  precision: number;
  version: number;
  label: string;
  disabled: boolean;
}>();
const emit = defineEmits<{ commit: [quantity: number, version: number] }>();
const draft = ref(String(props.quantity));
let editing = false;
let version = props.version;
watch(
  () => props.quantity,
  (value) => {
    if (!editing) draft.value = String(value);
  },
);
watch(
  () => props.disabled,
  (disabled) => {
    if (disabled) {
      editing = false;
      draft.value = String(props.quantity);
    }
  },
);
function focus() {
  editing = true;
  version = props.version;
}
function input(event: Event) {
  const element = event.target as HTMLInputElement;
  const pattern =
    props.precision > 0 ? new RegExp("^\\d*(?:[.,]\\d{0," + props.precision + "})?$") : /^\d*$/;
  if (pattern.test(element.value)) draft.value = element.value;
  else element.value = draft.value;
}
function commit() {
  editing = false;
  const text = draft.value.replace(",", ".");
  const quantity = Number(text);
  draft.value = String(props.quantity);
  if (
    !props.disabled &&
    text !== "" &&
    text !== "." &&
    Number.isFinite(quantity) &&
    quantity >= 0 &&
    quantity <= 99999999999.999 &&
    quantity !== props.quantity
  )
    emit("commit", quantity, version);
}
</script>

<template>
  <input
    class="cart-quantity"
    type="text"
    :inputmode="precision ? 'decimal' : 'numeric'"
    :aria-label="label"
    :value="draft"
    :disabled="disabled"
    autocomplete="off"
    @focus="focus"
    @input="input"
    @blur="commit"
    @keydown.enter.prevent="($event.target as HTMLInputElement).blur()"
  />
</template>

<style scoped>
.cart-quantity {
  width: 4.5rem;
  min-width: 0;
  padding: 0.4rem 0.2rem;
  text-align: center;
  font-size: 16px;
  border: 1px solid #ddd0e5;
  border-radius: 6px;
  background: white;
}
</style>
