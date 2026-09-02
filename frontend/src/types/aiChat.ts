export interface ChatRequest {
  sessionId: number | null;
  message: string;
}

export type ChatActionType = "CREATE_PANTRY_ITEM";
export type ChatActionStatus = "PENDING" | "CONFIRMED";

export interface CreatePantryItemPayload {
  name: string;
  quantity: number;
  unit: string;
  category?: string | null;
  expiryDate?: string | null;
}

export interface ProposedAction {
  actionId: number;
  type: ChatActionType;
  status: ChatActionStatus;
  payload: CreatePantryItemPayload;
}

export interface ChatResponse {
  sessionId: number;
  reply: string;
  proposedAction?: ProposedAction | null;
}
