"use client";

import { useActionState } from "react";
import type { EventResponse } from "@/api/generated/model";
import { Field, FormErrors, Notice, SubmitButton } from "@/components/form-parts";
import { updateEventAction } from "../../actions";
import { RequiresAccountField, TextareaField, VisibilityField } from "../../event-fields";

export function EditEventForm({ event }: { event: EventResponse }) {
    const [state, action] = useActionState(updateEventAction.bind(null, event.id ?? ""), {});
    const v = state.values ?? {};
    return (
        <form action={action} className="grid gap-5">
            <FormErrors errors={state.errors} />
            {state.done && <Notice>Değişiklikler kaydedildi.</Notice>}
            <Field
                label="Başlık"
                name="title"
                maxLength={150}
                required
                defaultValue={v.title ?? event.title}
            />
            <TextareaField
                label="Açıklama"
                name="description"
                maxLength={5000}
                defaultValue={v.description ?? event.description}
            />
            <VisibilityField defaultValue={v.visibility ?? event.visibility} />
            <RequiresAccountField
                defaultChecked={
                    v.requiresAccount ? v.requiresAccount === "on" : event.requiresAccount
                }
            />
            <SubmitButton pendingText="Kaydediliyor…" className="justify-self-start">
                Değişiklikleri kaydet
            </SubmitButton>
        </form>
    );
}
