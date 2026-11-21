"use client";

import { useActionState } from "react";
import { becomeOrganizerAction, logoutAction } from "@/auth/actions";
import { FormErrors, SubmitButton } from "@/components/form-parts";
import { Button } from "@/components/ui/button";

export function BecomeOrganizerForm() {
    const [state, action] = useActionState(becomeOrganizerAction, {});
    return (
        <form action={action} className="grid gap-4">
            <FormErrors errors={state.errors} />
            <SubmitButton pendingText="Hazırlanıyor…" className="justify-self-start">
                Organizatör ol
            </SubmitButton>
        </form>
    );
}

export function LogoutButton() {
    return (
        <form action={logoutAction}>
            <Button type="submit" variant="outline">
                Çıkış yap
            </Button>
        </form>
    );
}
