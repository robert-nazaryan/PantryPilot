export type QuantityParseResult =
  | { kind: "parsed"; quantity: number; unit: string; unitAssumed: boolean }
  | { kind: "empty" }
  | { kind: "unparseable" };

const DEFAULT_UNIT = "pcs";

const UNIT_SYNONYMS: ReadonlyArray<readonly [string, string[]]> = [
  ["pcs", ["pcs", "pc", "piece", "pieces", "count", "ct", "unit", "units"]],
  ["g", ["g", "gram", "grams", "gr"]],
  ["kg", ["kg", "kgs", "kilo", "kilos", "kilogram", "kilograms"]],
  ["ml", ["ml", "milliliter", "milliliters", "millilitre", "millilitres"]],
  ["l", ["l", "liter", "liters", "litre", "litres"]],
  ["tsp", ["tsp", "teaspoon", "teaspoons"]],
  ["tbsp", ["tbsp", "tbs", "tbl", "tablespoon", "tablespoons"]],
  ["cup", ["cup", "cups"]],
  ["oz", ["oz", "ounce", "ounces"]],
  ["lb", ["lb", "lbs", "pound", "pounds"]],
];

const CANONICAL_UNIT_BY_ALIAS: ReadonlyMap<string, string> = (() => {
  const map = new Map<string, string>();
  for (const [canonical, aliases] of UNIT_SYNONYMS) {
    for (const alias of aliases) {
      map.set(alias.toLowerCase(), canonical);
    }
  }
  return map;
})();

const LEADING_NUMBER_RE = /^\s*(\d+(?:[.,]\d+)?)\s*(.*)$/;

export function parseQuantityInput(raw: string): QuantityParseResult {
  const trimmed = raw.trim();
  if (trimmed === "") return { kind: "empty" };

  const match = LEADING_NUMBER_RE.exec(trimmed);
  if (!match) return { kind: "unparseable" };

  const quantity = Number(match[1].replace(",", "."));
  if (!Number.isFinite(quantity)) return { kind: "unparseable" };

  const rest = match[2].trim();
  if (rest === "") {
    return { kind: "parsed", quantity, unit: DEFAULT_UNIT, unitAssumed: true };
  }

  const canonical = CANONICAL_UNIT_BY_ALIAS.get(rest.toLowerCase());
  if (canonical) {
    return { kind: "parsed", quantity, unit: canonical, unitAssumed: false };
  }

  return { kind: "parsed", quantity, unit: rest, unitAssumed: false };
}

export function formatInitialQuantityInput(quantity: number, unit: string): string {
  const trimmedUnit = unit.trim();
  return trimmedUnit ? `${quantity} ${trimmedUnit}` : String(quantity);
}
