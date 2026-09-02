import { useState } from "react";
import type { ReactNode } from "react";
import { Sparkles, X } from "lucide-react";
import { AiChatPanel } from "./AiChatPanel";

export function AiChatButton(): ReactNode {
  const [open, setOpen] = useState(false);

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-label={open ? "Close assistant" : "Open assistant"}
        aria-expanded={open}
        aria-controls="ai-chat-panel"
        className={
          "fixed bottom-4 right-4 z-50 grid h-14 w-14 place-items-center rounded-full " +
          "bg-primary text-white shadow-lg transition-colors duration-150 hover:bg-primary-hover " +
          "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary " +
          "md:bottom-6 md:right-6"
        }
      >
        {open ? (
          <X className="h-6 w-6" aria-hidden />
        ) : (
          <Sparkles className="h-6 w-6" aria-hidden />
        )}
      </button>
      <AiChatPanel open={open} onClose={() => setOpen(false)} />
    </>
  );
}
