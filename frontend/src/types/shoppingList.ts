export interface ShoppingListItemResponse {
  id: number;
  name: string;
  quantity: number;
  unit: string | null;
  checked: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ShoppingListSummaryResponse {
  id: number;
  name: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ShoppingListResponse {
  id: number;
  name: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  items: ShoppingListItemResponse[];
}

export interface CreateShoppingListRequest {
  name?: string | null;
}

export interface UpdateShoppingListRequest {
  name?: string | null;
  active: boolean;
}

export interface CreateShoppingListItemRequest {
  name: string;
  quantity: number;
  unit?: string | null;
}

export interface UpdateShoppingListItemRequest {
  name: string;
  quantity: number;
  unit?: string | null;
  checked: boolean;
}

export interface ToggleShoppingListItemCheckedRequest {
  checked: boolean;
}
