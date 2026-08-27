export interface PantryItemResponse {
  id: number;
  name: string;
  quantity: number;
  unit: string;
  category: string | null;
  expiryDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePantryItemRequest {
  name: string;
  quantity: number;
  unit: string;
  category?: string | null;
  expiryDate?: string | null;
}

export interface UpdatePantryItemRequest {
  name: string;
  quantity: number;
  unit: string;
  category?: string | null;
  expiryDate?: string | null;
}

export interface ConsumeQuantityRequest {
  quantity: number;
}
