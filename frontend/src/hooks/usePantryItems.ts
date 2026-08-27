import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { UseQueryResult } from "@tanstack/react-query";
import * as api from "../api/pantryItems";
import type { PageResponse } from "../types/page";
import type {
  ConsumeQuantityRequest,
  CreatePantryItemRequest,
  PantryItemResponse,
  UpdatePantryItemRequest,
} from "../types/pantry";

export const PANTRY_ITEMS_QUERY_KEY = ["pantry-items"] as const;
export const DEFAULT_PAGE_SIZE = 20;

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

export function useConsumePantryItemMutation() {
  const invalidate = useInvalidatePantryLists();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: ConsumeQuantityRequest }) =>
      api.consumePantryItem(id, body),
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
