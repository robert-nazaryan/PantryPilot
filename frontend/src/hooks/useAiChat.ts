import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { UseMutationResult } from "@tanstack/react-query";
import * as api from "../api/aiChat";
import { PANTRY_ITEMS_QUERY_KEY } from "./usePantryItems";
import type { ApiError } from "../api/client";
import type { ChatRequest, ChatResponse, ConfirmActionResponse } from "../types/aiChat";

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
  ConfirmActionResponse,
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
