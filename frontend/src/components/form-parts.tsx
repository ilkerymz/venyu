"use client";

import { useFormStatus } from "react-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

type FieldProps = React.ComponentProps<typeof Input> & {
    label: string;
    name: string;
    hint?: string;
};

export function Field({ label, name, hint, className, ...input }: FieldProps) {
    const hintId = hint ? `${name}-hint` : undefined;
    return (
        <div className={cn("grid gap-2", className)}>
            <Label htmlFor={name}>{label}</Label>
            <Input
                id={name}
                name={name}
                aria-describedby={hintId}
                className="h-11 bg-card"
                {...input}
            />
            {hint && (
                <p id={hintId} className="text-sm text-muted-foreground">
                    {hint}
                </p>
            )}
        </div>
    );
}

export function FormErrors({ errors }: { errors?: string[] }) {
    if (!errors?.length) return null;
    return (
        <div
            role="alert"
            className="rounded-lg border border-destructive/30 bg-scarce-soft px-4 py-3 text-sm text-destructive"
        >
            {errors.length === 1 ? (
                <p>{errors[0]}</p>
            ) : (
                <ul className="list-disc space-y-1 pl-4">
                    {errors.map((e) => (
                        <li key={e}>{e}</li>
                    ))}
                </ul>
            )}
        </div>
    );
}

/** Gönderim sürerken butonu kilitler ve ne olduğunu söyler. */
export function SubmitButton({
    children,
    pendingText,
    className,
    ...props
}: React.ComponentProps<typeof Button> & { pendingText: string }) {
    const { pending } = useFormStatus();
    return (
        <Button
            type="submit"
            size="lg"
            className={cn("h-11", className)}
            disabled={pending}
            {...props}
        >
            {pending ? pendingText : children}
        </Button>
    );
}

export function Notice({ children }: { children: React.ReactNode }) {
    return (
        <div role="status" className="rounded-lg bg-success-soft px-4 py-3 text-sm text-success">
            {children}
        </div>
    );
}
