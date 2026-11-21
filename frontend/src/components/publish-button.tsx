"use client";

import { useActionState } from "react";
import { publishEventAction } from "@/app/panel/actions";
import { SubmitButton } from "@/components/form-parts";

/** Taslak etkinliği sayfasından yayınlar; hata olursa butonun yanında gösterir. */
export function PublishButton({ eventId }: { eventId: string }) {
    const [state, action] = useActionState(publishEventAction.bind(null, eventId), {});
    return (
        <form action={action} className="flex flex-wrap items-center gap-3">
            {state.errors?.map((error) => (
                <span key={error} role="alert" className="text-sm text-background/80">
                    {error}
                </span>
            ))}
            <SubmitButton size="sm" variant="secondary" className="h-9" pendingText="Yayınlanıyor…">
                Etkinliği yayınla
            </SubmitButton>
        </form>
    );
}
