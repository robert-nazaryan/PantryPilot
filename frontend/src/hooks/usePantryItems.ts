import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { UseQueryResult } from "@tanstack/react-query";
import * as api from "../api/pantryItems";
import type { PageResponse } from "../types/page";
import type {
  CreatePantryItemRequest,
  PantryItemResponse,
  UpdatePantryItemRequest,
} from "../types/pantry";

export const PANTRY_ITEMS_QUERY_KEY = ["pantry-items"] as const;
export const DEFAULT_PAGE_SIZE = 20;
const SUGGESTIONS_PAGE_SIZE = 500;

interface UsePantryItemsQueryOptions {
  page: number;
  size?: number;
}

export function usePantryItemsQuery({
  page,
  size = DEFAULT_PAGE_SIZE,
}: UsePantryItemsQueryOptions): UseQueryResult<PageResponse<PantryItemResponse>> {
  return useQuery({
    queryKey: [...PANTRY_ITEMS_QUERY_KEY, "list", { page, size }],
    queryFn: () => api.listPantryItems({ page, size }),
    placeholderData: (previous) => previous,
  });
}

export function usePantryItemQuery(id: number | null): UseQueryResult<PantryItemResponse> {
  return useQuery({
    queryKey: [...PANTRY_ITEMS_QUERY_KEY, "detail", id],
    queryFn: () => api.getPantryItem(id as number),
    enabled: id !== null,
  });
}

export function useExpiringPantryItemsQuery(
  days?: number,
): UseQueryResult<PantryItemResponse[]> {
  return useQuery({
    queryKey: [...PANTRY_ITEMS_QUERY_KEY, "expiring", { days: days ?? "default" }],
    queryFn: () => api.listExpiringPantryItems(days),
  });
}

function useInvalidatePantryLists() {
  const qc = useQueryClient();
  return () => qc.invalidateQueries({ queryKey: PANTRY_ITEMS_QUERY_KEY });
}

export function useCreatePantryItemMutation() {
  const invalidate = useInvalidatePantryLists();
  return useMutation({
    mutationFn: (body: CreatePantryItemRequest) => api.createPantryItem(body),
    onSuccess: () => invalidate(),
  });
}

export function useUpdatePantryItemMutation() {
  const invalidate = useInvalidatePantryLists();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdatePantryItemRequest }) =>
      api.updatePantryItem(id, body),
    onSuccess: () => invalidate(),
  });
}

export function useDeletePantryItemMutation() {
  const invalidate = useInvalidatePantryLists();
  return useMutation({
    mutationFn: (id: number) => api.deletePantryItem(id),
    onSuccess: () => invalidate(),
  });
}

function distinctFrequencySort(values: string[]): string[] {
  const counts = new Map<string, { key: string; count: number }>();
  for (const raw of values) {
    if (!raw) continue;
    const trimmed = raw.trim();
    if (!trimmed) continue;
    const key = trimmed.toLowerCase();
    const existing = counts.get(key);
    if (existing) existing.count += 1;
    else counts.set(key, { key: trimmed, count: 1 });
  }
  return Array.from(counts.values())
    .sort((a, b) => b.count - a.count || a.key.localeCompare(b.key))
    .map((c) => c.key);
}

export function useDistinctPantryUnits(): UseQueryResult<string[]> {
  return useQuery({
    queryKey: [...PANTRY_ITEMS_QUERY_KEY, "list", { page: 0, size: SUGGESTIONS_PAGE_SIZE }],
    queryFn: () => api.listPantryItems({ page: 0, size: SUGGESTIONS_PAGE_SIZE }),
    select: (data) => distinctFrequencySort(data.content.map((i) => i.unit)),
    staleTime: 60_000,
  });
}

export function useDistinctPantryCategories(): UseQueryResult<string[]> {
  return useQuery({
    queryKey: [...PANTRY_ITEMS_QUERY_KEY, "list", { page: 0, size: SUGGESTIONS_PAGE_SIZE }],
    queryFn: () => api.listPantryItems({ page: 0, size: SUGGESTIONS_PAGE_SIZE }),
    select: (data) =>
      distinctFrequencySort(data.content.map((i) => i.category ?? "").filter(Boolean)),
    staleTime: 60_000,
  });
}
