"use client";

import Link from "next/link";
import { useActionState } from "react";
import { verifyEmailAction } from "@/auth/actions";
import { FormErrors, Notice, SubmitButton } from "@/components/form-parts";
import { Button } from "@/components/ui/button";

export function VerifyEmailForm({ token }: { token: string }) {
    const [state, action] = useActionState(verifyEmailAction, {});
    if (state.done) {
        return (
            <div className="grid gap-6">
                <Notice>E-posta adresin doğrulandı.</Notice>
                <Button asChild className="justify-self-start">
                    <Link href="/hesabim">Hesabıma git</Link>
                </Button>
            </div>
        );
    }
    return (
        <form action={action} className="grid gap-5">
            <FormErrors errors={state.errors} />
            <input type="hidden" name="token" value={token} />
            <SubmitButton pendingText="Doğrulanıyor…" className="justify-self-start">
                E-postamı doğrula
            </SubmitButton>
        </form>
    );
}
