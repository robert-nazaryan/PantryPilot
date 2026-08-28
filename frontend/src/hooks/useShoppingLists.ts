import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { UseQueryResult } from "@tanstack/react-query";
import * as listsApi from "../api/shoppingLists";
import * as itemsApi from "../api/shoppingListItems";
import type { PageResponse } from "../types/page";
import type {
  CreateShoppingListItemRequest,
  CreateShoppingListRequest,
  ShoppingListItemResponse,
  ShoppingListResponse,
  ShoppingListSummaryResponse,
  UpdateShoppingListItemRequest,
  UpdateShoppingListRequest,
} from "../types/shoppingList";

export const SHOPPING_LISTS_QUERY_KEY = ["shopping-lists"] as const;
export const DEFAULT_PAGE_SIZE = 20;

interface UseShoppingListsQueryOptions {
  page: number;
  size?: number;
}

export function useShoppingListsQuery({
  page,
  size = DEFAULT_PAGE_SIZE,
}: UseShoppingListsQueryOptions): UseQueryResult<
  PageResponse<ShoppingListSummaryResponse>
> {
  return useQuery({
    queryKey: [...SHOPPING_LISTS_QUERY_KEY, "list", { page, size }],
    queryFn: () => listsApi.listShoppingLists({ page, size }),
    placeholderData: (previous) => previous,
  });
}

export function useShoppingListQuery(
  id: number | null,
): UseQueryResult<ShoppingListResponse> {
  return useQuery({
    queryKey: [...SHOPPING_LISTS_QUERY_KEY, "detail", id],
    queryFn: () => listsApi.getShoppingList(id as number),
    enabled: id !== null,
    retry: (failureCount, error) => {
      const status = (error as { status?: number } | null)?.status;
      if (status === 404) return false;
      return failureCount < 3;
    },
  });
}

function useInvalidateShoppingListLists() {
  const qc = useQueryClient();
  return () =>
    qc.invalidateQueries({ queryKey: [...SHOPPING_LISTS_QUERY_KEY, "list"] });
}

function useInvalidateShoppingListDetail() {
  const qc = useQueryClient();
  return (id: number) =>
    qc.invalidateQueries({
      queryKey: [...SHOPPING_LISTS_QUERY_KEY, "detail", id],
    });
}

export function useCreateShoppingListMutation() {
  const invalidateLists = useInvalidateShoppingListLists();
  return useMutation({
    mutationFn: (body: CreateShoppingListRequest) =>
      listsApi.createShoppingList(body),
    onSuccess: () => invalidateLists(),
  });
}

export function useUpdateShoppingListMutation() {
  const invalidateLists = useInvalidateShoppingListLists();
  const invalidateDetail = useInvalidateShoppingListDetail();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdateShoppingListRequest }) =>
      listsApi.updateShoppingList(id, body),
    onSuccess: (data) => {
      invalidateLists();
      invalidateDetail(data.id);
    },
  });
}

export function useDeleteShoppingListMutation() {
  const qc = useQueryClient();
  const invalidateLists = useInvalidateShoppingListLists();
  return useMutation({
    mutationFn: (id: number) => listsApi.deleteShoppingList(id),
    onSuccess: (_data, id) => {
      invalidateLists();
      qc.removeQueries({
        queryKey: [...SHOPPING_LISTS_QUERY_KEY, "detail", id],
      });
    },
  });
}

export function useAddShoppingListItemMutation() {
  const invalidateDetail = useInvalidateShoppingListDetail();
  return useMutation({
    mutationFn: ({
      listId,
      body,
    }: {
      listId: number;
      body: CreateShoppingListItemRequest;
    }): Promise<ShoppingListItemResponse> =>
      itemsApi.addShoppingListItem(listId, body),
    onSuccess: (_data, variables) => invalidateDetail(variables.listId),
  });
}

export function useUpdateShoppingListItemMutation() {
  const invalidateDetail = useInvalidateShoppingListDetail();
  return useMutation({
    mutationFn: ({
      listId,
      itemId,
      body,
    }: {
      listId: number;
      itemId: number;
      body: UpdateShoppingListItemRequest;
    }): Promise<ShoppingListItemResponse> =>
      itemsApi.updateShoppingListItem(listId, itemId, body),
    onSuccess: (_data, variables) => invalidateDetail(variables.listId),
  });
}

interface ToggleVariables {
  listId: number;
  itemId: number;
  checked: boolean;
}

export function useToggleShoppingListItemCheckedMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ listId, itemId, checked }: ToggleVariables) =>
      itemsApi.setShoppingListItemChecked(listId, itemId, { checked }),
    onMutate: async ({ listId, itemId, checked }) => {
      const key = [...SHOPPING_LISTS_QUERY_KEY, "detail", listId];
      await qc.cancelQueries({ queryKey: key });
      const previous = qc.getQueryData<ShoppingListResponse>(key);
      if (previous) {
        qc.setQueryData<ShoppingListResponse>(key, {
          ...previous,
          items: previous.items.map((item) =>
            item.id === itemId ? { ...item, checked } : item,
          ),
        });
      }
      return { previous };
    },
    onError: (_err, variables, context) => {
      if (context?.previous) {
        const key = [
          ...SHOPPING_LISTS_QUERY_KEY,
          "detail",
          variables.listId,
        ];
        qc.setQueryData(key, context.previous);
      }
    },
    onSettled: (_data, _err, variables) => {
      qc.invalidateQueries({
        queryKey: [...SHOPPING_LISTS_QUERY_KEY, "detail", variables.listId],
      });
    },
  });
}

export function useDeleteShoppingListItemMutation() {
  const invalidateDetail = useInvalidateShoppingListDetail();
  return useMutation({
    mutationFn: ({ listId, itemId }: { listId: number; itemId: number }) =>
      itemsApi.deleteShoppingListItem(listId, itemId),
    onSuccess: (_data, variables) => invalidateDetail(variables.listId),
  });
}
