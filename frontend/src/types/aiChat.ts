export interface ChatRequest {
  sessionId: number | null;
  message: string;
}

export type ChatActionType =
  | "CREATE_PANTRY_ITEM"
  | "UPDATE_PANTRY_ITEM"
  | "DELETE_PANTRY_ITEM"
  | "CONSUME_PANTRY_ITEM";

export type ChatActionStatus = "PENDING" | "CONFIRMED";

export interface CreatePantryItemPayload {
  name: string;
  quantity: number;
  unit: string;
  category?: string | null;
  expiryDate?: string | null;
}

export interface UpdatePantryItemPayload {
  itemId: number;
  name: string;
  quantity: number;
  unit: string;
  category?: string | null;
  expiryDate?: string | null;
}

export interface DeletePantryItemPayload {
  itemId: number;
  name: string;
}

export interface ConsumePantryItemPayload {
  itemId: number;
  name: string;
  quantity: number;
  unit: string;
  availableQuantity: number;
}

interface ProposedActionBase {
  actionId: number;
  status: ChatActionStatus;
}

export type ProposedAction =
  | (ProposedActionBase & { type: "CREATE_PANTRY_ITEM"; payload: CreatePantryItemPayload })
  | (ProposedActionBase & { type: "UPDATE_PANTRY_ITEM"; payload: UpdatePantryItemPayload })
  | (ProposedActionBase & { type: "DELETE_PANTRY_ITEM"; payload: DeletePantryItemPayload })
  | (ProposedActionBase & { type: "CONSUME_PANTRY_ITEM"; payload: ConsumePantryItemPayload });

export interface ChatResponse {
  sessionId: number;
  reply: string;
  proposedAction?: ProposedAction | null;
}

export interface ConfirmActionResponse {
  actionType: ChatActionType;
  item: import("./pantry").PantryItemResponse | null;
}
