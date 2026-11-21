"use client";

import { useActionState, useState, useSyncExternalStore } from "react";
import type { BookingView } from "../../data";
import { formatEventDay, formatTimeRange } from "@/lib/format";
import { cn } from "@/lib/utils";
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

type Props = {
    /** Onaylı ve henüz etkinliğe bağlanmamış mekan rezervasyonları. */
    bookings: BookingView[];
    preselected?: string;
};

export function NewEventForm({ bookings, preselected }: Props) {
    const today = useSyncExternalStore(noop, istanbulToday, () => undefined);
    const [state, action] = useActionState(createEventAction, {});
    const v = state.values ?? {};
    const initialBooking =
        v.venueBookingId ??
        (bookings.some((b) => b.booking.id === preselected) ? preselected : undefined);
    const [mode, setMode] = useState<"venue" | "address">(initialBooking ? "venue" : "address");
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
                {bookings.length > 0 && (
                    <div
                        role="radiogroup"
                        aria-label="Etkinlik nerede"
                        className="flex w-fit rounded-lg bg-muted p-1"
                    >
                        {(
                            [
                                ["venue", "Venyu mekanında"],
                                ["address", "Kendi adresimde"],
                            ] as const
                        ).map(([value, label]) => (
                            <button
                                key={value}
                                type="button"
                                role="radio"
                                aria-checked={mode === value}
                                onClick={() => setMode(value)}
                                className={cn(
                                    "rounded-md px-3 py-1.5 text-sm font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50",
                                    mode === value ? "bg-card shadow-xs" : "text-muted-foreground",
                                )}
                            >
                                {label}
                            </button>
                        ))}
                    </div>
                )}
                {mode === "venue" ? (
                    <fieldset className="grid gap-3">
                        <legend className="mb-1 text-sm font-medium">Onaylı rezervasyonun</legend>
                        {bookings.map(({ booking, venue, slot }) => (
                            <label
                                key={booking.id}
                                className="flex cursor-pointer gap-3 rounded-lg border border-input p-4 has-[:checked]:border-primary has-[:checked]:bg-secondary/50"
                            >
                                <input
                                    type="radio"
                                    name="venueBookingId"
                                    value={booking.id}
                                    required
                                    defaultChecked={booking.id === initialBooking}
                                    className="mt-1 size-4 accent-primary"
                                />
                                <span>
                                    <span className="block font-medium">
                                        {venue?.name}
                                        {venue?.district && (
                                            <span className="font-normal text-muted-foreground">
                                                , {venue.district}
                                            </span>
                                        )}
                                    </span>
                                    {slot && (
                                        <span className="mt-0.5 block text-sm text-muted-foreground nums">
                                            {formatEventDay(slot.startsAt ?? "")},{" "}
                                            {formatTimeRange(
                                                slot.startsAt ?? "",
                                                slot.endsAt ?? "",
                                            )}
                                        </span>
                                    )}
                                </span>
                            </label>
                        ))}
                        <p className="text-sm text-muted-foreground">
                            Tarih, saat ve adres rezervasyondan gelir; biletlerin toplamı mekanın
                            kapasitesini aşamaz.
                        </p>
                    </fieldset>
                ) : (
                    <>
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
                            Saatler İstanbul saatidir. Bitiş başlangıçtan önceyse ertesi gün
                            sayılır.
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
                    </>
                )}
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
