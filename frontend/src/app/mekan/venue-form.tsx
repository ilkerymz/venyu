"use client";

import { useActionState } from "react";
import type { VenueResponse } from "@/api/generated/model";
import type { FormState } from "@/auth/actions";
import { Field, FormErrors, Notice, SubmitButton } from "@/components/form-parts";
import { TextareaField } from "@/app/panel/event-fields";

type Props = {
    action: (state: FormState, form: FormData) => Promise<FormState>;
    venue?: VenueResponse;
    submitLabel: string;
};

export function VenueForm({ action, venue, submitLabel }: Props) {
    const [state, formAction] = useActionState(action, {});
    const v = state.values ?? {};
    return (
        <form action={formAction} className="grid max-w-2xl gap-5">
            <FormErrors errors={state.errors} />
            {state.done && <Notice>Mekan bilgileri kaydedildi.</Notice>}
            <Field
                label="Mekanın adı"
                name="name"
                maxLength={100}
                required
                defaultValue={v.name ?? venue?.name}
                placeholder="Örn. Kafe Lale"
            />
            <TextareaField
                label="Tanıtım"
                name="description"
                maxLength={2000}
                defaultValue={v.description ?? venue?.description}
                hint="Organizatörler mekan sayfasında görür: atmosfer, ekipman, uygun etkinlik türleri. İsteğe bağlı."
            />
            <Field
                label="Adres"
                name="address"
                maxLength={300}
                required
                defaultValue={v.address ?? venue?.address}
                placeholder="Sokak, numara"
            />
            <div className="grid gap-5 sm:grid-cols-2">
                <Field
                    label="Semt"
                    name="district"
                    maxLength={100}
                    required
                    defaultValue={v.district ?? venue?.district}
                    placeholder="Örn. Moda"
                />
                <Field
                    label="Şehir"
                    name="city"
                    maxLength={100}
                    required
                    defaultValue={v.city ?? venue?.city ?? "İstanbul"}
                />
            </div>
            <Field
                label="Kapasite"
                name="capacity"
                type="number"
                min={1}
                max={10000}
                defaultValue={v.capacity ?? venue?.capacity}
                className="max-w-48"
                hint="Aynı anda ağırlayabileceğin kişi sayısı. Mekandaki etkinliklerin toplam bilet sayısı bunu aşamaz."
            />
            <SubmitButton pendingText="Kaydediliyor…" className="justify-self-start">
                {submitLabel}
            </SubmitButton>
        </form>
    );
}
