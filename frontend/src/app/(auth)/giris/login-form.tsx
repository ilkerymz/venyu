"use client";

import Link from "next/link";
import { useActionState } from "react";
import { loginAction } from "@/auth/actions";
import { Field, FormErrors, SubmitButton } from "@/components/form-parts";

export function LoginForm({ next }: { next?: string }) {
    const [state, action] = useActionState(loginAction, {});
    return (
        <form action={action} className="grid gap-5">
            <FormErrors errors={state.errors} />
            {next && <input type="hidden" name="next" value={next} />}
            <Field
                label="E-posta"
                name="email"
                defaultValue={state.values?.email}
                type="email"
                autoComplete="email"
                required
            />
            <div className="grid gap-2">
                <Field
                    label="Şifre"
                    name="password"
                    type="password"
                    autoComplete="current-password"
                    required
                />
                <Link
                    href="/sifremi-unuttum"
                    className="justify-self-end text-sm text-primary underline-offset-4 hover:underline"
                >
                    Şifremi unuttum
                </Link>
            </div>
            <SubmitButton pendingText="Giriş yapılıyor…">Giriş yap</SubmitButton>
        </form>
    );
}
