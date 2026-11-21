"use client";

import { useActionState, useState } from "react";
import { FormErrors, SubmitButton } from "@/components/form-parts";
import { Button } from "@/components/ui/button";
import { cancelEventAction } from "../../actions";
import { TextareaField } from "../../event-fields";

/** Geri alınamaz işlem: önce ne olacağını anlatır, sonra ikinci bir onay ister. */
export function CancelEvent({ eventId }: { eventId: string }) {
    const [open, setOpen] = useState(false);
    const [state, action] = useActionState(cancelEventAction.bind(null, eventId), {});

    if (!open) {
        return (
            <div className="grid gap-3">
                <p className="leading-relaxed text-muted-foreground">
                    İptal geri alınamaz. Bütün kayıtlar iptal edilir ve katılımcılara e-posta gider.
                </p>
                <Button
                    variant="outline"
                    className="justify-self-start text-destructive"
                    onClick={() => setOpen(true)}
                >
                    Etkinliği iptal et
                </Button>
            </div>
        );
    }
    return (
        <form action={action} className="grid gap-4">
            <FormErrors errors={state.errors} />
            <TextareaField
                label="İptal nedeni"
                name="reason"
                maxLength={500}
                hint="İsteğe bağlı. Katılımcılara gönderilen e-postada yer alır."
            />
            <div className="flex flex-wrap gap-2">
                <SubmitButton
                    variant="destructive"
                    pendingText="İptal ediliyor…"
                    size="default"
                    className="h-9"
                >
                    Evet, etkinliği iptal et
                </SubmitButton>
                <Button type="button" variant="ghost" onClick={() => setOpen(false)}>
                    Vazgeç
                </Button>
            </div>
        </form>
    );
}
