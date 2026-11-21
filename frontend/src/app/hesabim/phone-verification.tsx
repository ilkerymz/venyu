"use client";

import { useActionState, useState } from "react";
import { Field, FormErrors, Notice, SubmitButton } from "@/components/form-parts";
import { Button } from "@/components/ui/button";
import { formatPhone } from "@/lib/phone";
import { confirmPhoneCodeAction, type PhoneState, requestPhoneCodeAction } from "./phone-actions";

/** İki adım: numara → SMS kodu. Kodu gönderdikten sonra numarayı değiştirmek ilk adıma döner. */
export function PhoneVerification({ current }: { current?: string }) {
    const [sent, setSent] = useState<string>();
    const [requestState, request] = useActionState(async (s: PhoneState, f: FormData) => {
        const result = await requestPhoneCodeAction(s, f);
        if (result.sentTo) setSent(result.sentTo);
        return result;
    }, {});

    if (sent) return <CodeForm sentTo={sent} onChangeNumber={() => setSent(undefined)} />;

    return (
        <form action={request} className="grid gap-4">
            <FormErrors errors={requestState.errors} />
            <Field
                label="Cep telefonu"
                name="phone"
                type="tel"
                inputMode="tel"
                autoComplete="tel"
                placeholder="0532 123 45 67"
                defaultValue={requestState.input ?? current}
                required
                className="max-w-xs"
            />
            <SubmitButton pendingText="Gönderiliyor…" className="justify-self-start">
                Doğrulama kodu gönder
            </SubmitButton>
        </form>
    );
}

type CodeState = PhoneState & { resent?: boolean };

function CodeForm({ sentTo, onChangeNumber }: { sentTo: string; onChangeNumber: () => void }) {
    // Tek form, iki niyet: kodu doğrula ya da yeni kod iste (submitter'ın name/value'su FormData'ya girer).
    const [state, action] = useActionState(
        async (prev: CodeState, form: FormData): Promise<CodeState> =>
            form.get("intent") === "resend"
                ? { ...(await requestPhoneCodeAction(prev, form)), resent: true }
                : confirmPhoneCodeAction(sentTo, prev, form),
        {},
    );

    if (state.verified) return <Notice>Telefon numaran doğrulandı.</Notice>;

    return (
        <form action={action} className="grid gap-4">
            <p className="text-sm text-muted-foreground">
                {formatPhone(sentTo)} numarasına 6 haneli bir kod gönderdik. Kod 5 dakika geçerli.
            </p>
            <FormErrors errors={state.errors} />
            {state.resent && !state.errors && <Notice>Yeni kod gönderildi.</Notice>}
            <input type="hidden" name="phone" value={sentTo} />
            <Field
                label="Doğrulama kodu"
                name="code"
                inputMode="numeric"
                autoComplete="one-time-code"
                pattern="[0-9]{6}"
                maxLength={6}
                required
                className="max-w-40"
            />
            <div className="flex flex-wrap items-center gap-2">
                <SubmitButton name="intent" value="confirm" pendingText="Gönderiliyor…">
                    Doğrula
                </SubmitButton>
                <Button
                    type="submit"
                    name="intent"
                    value="resend"
                    variant="ghost"
                    size="sm"
                    formNoValidate
                >
                    Kodu tekrar gönder
                </Button>
                <Button type="button" variant="ghost" size="sm" onClick={onChangeNumber}>
                    Numarayı değiştir
                </Button>
            </div>
        </form>
    );
}
