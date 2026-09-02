import type { ReactNode } from "react";
import { Outlet } from "react-router-dom";
import { AiChatButton } from "./AiChatButton";
import { Navbar } from "./Navbar";

export function AuthenticatedLayout(): ReactNode {
  return (
    <div className="min-h-dvh bg-surface-page dark:bg-surface-page-dark">
      <Navbar />
      <main className="mx-auto max-w-[1200px] px-4 py-6 md:px-6 md:py-10">
        <Outlet />
      </main>
      <AiChatButton />
    </div>
  );
}
