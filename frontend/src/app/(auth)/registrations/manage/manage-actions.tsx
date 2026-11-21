"use client";

import { useActionState, useState } from "react";
import { FormErrors } from "@/components/form-parts";
import { Button } from "@/components/ui/button";
import { cancelManagedAction } from "../actions";

export function CancelManagedRegistration({ token }: { token: string }) {
    const [confirming, setConfirming] = useState(false);
    const [state, action, pending] = useActionState(cancelManagedAction.bind(null, token), {});
    if (!confirming) {
        return (
            <Button
                variant="outline"
                className="justify-self-start"
                onClick={() => setConfirming(true)}
            >
                Kaydımı iptal et
            </Button>
        );
    }
    return (
        <form action={action} className="grid gap-3">
            <FormErrors errors={state.errors} />
            <p className="text-sm">
                Kaydını iptal edersen yerin başkasına açılır. Bu işlem geri alınamaz.
            </p>
            <div className="flex gap-2">
                <Button type="submit" variant="destructive" disabled={pending}>
                    {pending ? "İptal ediliyor…" : "Evet, iptal et"}
                </Button>
                <Button type="button" variant="ghost" onClick={() => setConfirming(false)}>
                    Vazgeç
                </Button>
            </div>
        </form>
    );
}
