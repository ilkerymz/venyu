"use client";

import type { FormField } from "@/api/generated/model";
import { Field } from "@/components/form-parts";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

/** Kayıt formundaki tek bir soru; kayıt panelinde ve form oluşturucunun önizlemesinde aynı. */
export function QuestionField({ field }: { field: FormField }) {
    const id = `q-${field.key}`;
    // Label bileşeni flex; etiket ve "(isteğe bağlı)" tek parça kalsın diye tek span içinde.
    const label = (
        <span>
            {field.label}
            {!field.required && (
                <span className="font-normal text-muted-foreground"> (isteğe bağlı)</span>
            )}
        </span>
    );
    const options = field.options ?? [];

    switch (field.type) {
        case "LONG_TEXT":
            return (
                <div className="grid gap-2">
                    <Label htmlFor={id}>{label}</Label>
                    <Textarea
                        id={id}
                        name={field.key}
                        required={field.required}
                        maxLength={5000}
                        className="min-h-24 bg-card"
                    />
                </div>
            );
        case "BOOLEAN":
            return (
                <label className="flex cursor-pointer items-start gap-3 text-sm font-medium">
                    <input
                        type="checkbox"
                        name={field.key}
                        required={field.required}
                        className="mt-0.5 size-4 accent-primary"
                    />
                    <span>{label}</span>
                </label>
            );
        case "SINGLE_CHOICE":
        case "MULTI_CHOICE": {
            const multi = field.type === "MULTI_CHOICE";
            return (
                <fieldset className="grid gap-2">
                    <legend className="mb-1 text-sm font-medium">{label}</legend>
                    {options.map((option) => (
                        <label
                            key={option}
                            className="flex cursor-pointer items-center gap-3 text-sm"
                        >
                            <input
                                type={multi ? "checkbox" : "radio"}
                                name={field.key}
                                value={option}
                                required={field.required && !multi}
                                className="size-4 accent-primary"
                            />
                            {option}
                        </label>
                    ))}
                </fieldset>
            );
        }
        default:
            return (
                <Field
                    label={field.label + (field.required ? "" : " (isteğe bağlı)")}
                    id={id}
                    name={field.key}
                    type={field.type === "NUMBER" ? "number" : "text"}
                    inputMode={field.type === "NUMBER" ? "decimal" : undefined}
                    step={field.type === "NUMBER" ? "any" : undefined}
                    maxLength={field.type === "TEXT" ? 500 : undefined}
                    required={field.required}
                />
            );
    }
}
