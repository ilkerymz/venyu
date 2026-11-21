"use client";

import { useActionState } from "react";
import { registerAction } from "@/auth/actions";
import { Field, FormErrors, SubmitButton } from "@/components/form-parts";

export function RegisterForm() {
    const [state, action] = useActionState(registerAction, {});
    return (
        <form action={action} className="grid gap-5">
            <FormErrors errors={state.errors} />
            <Field
                label="Ad soyad"
                name="displayName"
                defaultValue={state.values?.displayName}
                autoComplete="name"
                maxLength={50}
                required
                hint="Etkinlik sayfalarında ve katılımcı listelerinde bu ad görünür."
            />
            <Field
                label="E-posta"
                name="email"
                defaultValue={state.values?.email}
                type="email"
                autoComplete="email"
                maxLength={254}
                required
            />
            <Field
                label="Şifre"
                name="password"
                type="password"
                autoComplete="new-password"
                minLength={8}
                maxLength={72}
                required
                hint="En az 8 karakter."
            />
            <SubmitButton pendingText="Hesap oluşturuluyor…">Hesap oluştur</SubmitButton>
        </form>
    );
}
