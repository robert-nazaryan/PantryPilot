import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { UseMutationResult } from "@tanstack/react-query";
import * as api from "../api/aiChat";
import { PANTRY_ITEMS_QUERY_KEY } from "./usePantryItems";
import type { ApiError } from "../api/client";
import type { ChatRequest, ChatResponse } from "../types/aiChat";
import type { PantryItemResponse } from "../types/pantry";

export function useSendChatMessageMutation(): UseMutationResult<
  ChatResponse,
  ApiError,
  ChatRequest
> {
  return useMutation({
    mutationFn: (body: ChatRequest) => api.sendChatMessage(body),
  });
}

export function useConfirmChatActionMutation(): UseMutationResult<
  PantryItemResponse,
  ApiError,
  number
> {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (actionId: number) => api.confirmChatAction(actionId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: PANTRY_ITEMS_QUERY_KEY });
    },
  });
}
