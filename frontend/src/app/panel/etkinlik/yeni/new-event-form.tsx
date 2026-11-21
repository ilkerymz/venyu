"use client";

import { useActionState, useSyncExternalStore } from "react";
import { Field, FormErrors, SubmitButton } from "@/components/form-parts";
import { createEventAction } from "../../actions";
import {
    FormSection,
    RequiresAccountField,
    TextareaField,
    VisibilityField,
} from "../../event-fields";

/** Bugünün tarihi (İstanbul) sadece tarayıcıda hesaplanır; sayfa böylece tamamen statik kalır. */
const noop = () => () => {};
const istanbulToday = () =>
    new Intl.DateTimeFormat("en-CA", { timeZone: "Europe/Istanbul" }).format(new Date());

export function NewEventForm() {
    const today = useSyncExternalStore(noop, istanbulToday, () => undefined);
    const [state, action] = useActionState(createEventAction, {});
    const v = state.values ?? {};
    return (
        <form action={action} className="grid gap-6">
            <FormErrors errors={state.errors} />

            <FormSection title="Temel bilgiler">
                <Field
                    label="Başlık"
                    name="title"
                    maxLength={150}
                    required
                    defaultValue={v.title}
                    placeholder="Örn. Seramik atölyesi: ilk kupan"
                />
                <TextareaField
                    label="Açıklama"
                    name="description"
                    maxLength={5000}
                    defaultValue={v.description}
                    hint="Ne yapılacak, kimler için, yanında ne getirmeli? İsteğe bağlı."
                />
            </FormSection>

            <FormSection title="Zaman ve yer">
                <div className="grid gap-5 sm:grid-cols-3">
                    <Field
                        label="Tarih"
                        name="date"
                        type="date"
                        min={today}
                        required
                        defaultValue={v.date}
                    />
                    <Field
                        label="Başlangıç"
                        name="startTime"
                        type="time"
                        required
                        defaultValue={v.startTime ?? "19:00"}
                    />
                    <Field
                        label="Bitiş"
                        name="endTime"
                        type="time"
                        required
                        defaultValue={v.endTime ?? "21:00"}
                    />
                </div>
                <p className="-mt-2 text-sm text-muted-foreground">
                    Saatler İstanbul saatidir. Bitiş başlangıçtan önceyse ertesi gün sayılır.
                </p>
                <TextareaField
                    label="Adres"
                    name="customAddress"
                    maxLength={300}
                    required
                    defaultValue={v.customAddress}
                    className="min-h-20 bg-card"
                    hint="İlk satır etkinlik sayfasının başlığında görünür; örneğin mekanın adı."
                />
            </FormSection>

            <FormSection title="Kimler katılabilir">
                <VisibilityField defaultValue={v.visibility} />
                <RequiresAccountField defaultChecked={v.requiresAccount === "on"} />
            </FormSection>

            <div>
                <SubmitButton pendingText="Oluşturuluyor…">Taslağı oluştur</SubmitButton>
                <p className="mt-3 text-sm text-muted-foreground">
                    Etkinlik taslak olarak kaydedilir; biletleri ekleyip yayınladığında sayfası
                    açılır.
                </p>
            </div>
        </form>
    );
}
