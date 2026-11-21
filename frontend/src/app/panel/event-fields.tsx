"use client";

import { VISIBILITY } from "@/components/event-status-badge";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";

export function FormSection({ title, children }: { title: string; children: React.ReactNode }) {
    return (
        <fieldset className="grid gap-5 rounded-xl border bg-card p-6">
            <legend className="sr-only">{title}</legend>
            <h2 aria-hidden className="text-xl font-semibold">
                {title}
            </h2>
            {children}
        </fieldset>
    );
}

export function TextareaField({
    label,
    name,
    hint,
    ...props
}: React.ComponentProps<typeof Textarea> & { label: string; name: string; hint?: string }) {
    return (
        <div className="grid gap-2">
            <Label htmlFor={name}>{label}</Label>
            <Textarea
                id={name}
                name={name}
                className="min-h-28 bg-card"
                aria-describedby={hint ? `${name}-hint` : undefined}
                {...props}
            />
            {hint && (
                <p id={`${name}-hint`} className="text-sm text-muted-foreground">
                    {hint}
                </p>
            )}
        </div>
    );
}

/** Görünürlük seçimi: üç seçenek, her biri ne anlama geldiğini söyler. */
export function VisibilityField({ defaultValue = "PUBLIC" }: { defaultValue?: string }) {
    return (
        <div role="radiogroup" aria-label="Görünürlük" className="grid gap-3">
            {Object.entries(VISIBILITY).map(([value, option]) => (
                <label
                    key={value}
                    className={cn(
                        "flex cursor-pointer gap-3 rounded-lg border border-input p-4",
                        "has-[:checked]:border-primary has-[:checked]:bg-secondary/50",
                        "has-[:focus-visible]:ring-[3px] has-[:focus-visible]:ring-ring/50",
                    )}
                >
                    <input
                        type="radio"
                        name="visibility"
                        value={value}
                        defaultChecked={value === defaultValue}
                        className="mt-1 size-4 accent-primary"
                    />
                    <span>
                        <span className="block font-medium">{option.label}</span>
                        <span className="mt-0.5 block text-sm text-muted-foreground">
                            {option.hint}
                        </span>
                    </span>
                </label>
            ))}
        </div>
    );
}

export function RequiresAccountField({ defaultChecked }: { defaultChecked?: boolean }) {
    return (
        <label className="flex cursor-pointer items-start gap-3">
            <input
                type="checkbox"
                name="requiresAccount"
                defaultChecked={defaultChecked}
                className="mt-1 size-4 accent-primary"
            />
            <span>
                <span className="block font-medium">Katılmak için Venyu hesabı gerekli</span>
                <span className="mt-0.5 block text-sm text-muted-foreground">
                    Kapalıysa hesabı olmayanlar da ad ve e-postayla kayıt olabilir.
                </span>
            </span>
        </label>
    );
}
