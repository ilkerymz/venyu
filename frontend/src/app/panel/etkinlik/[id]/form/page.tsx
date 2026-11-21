import type { Metadata } from "next";
import { get2 as getForm } from "@/api/generated/form-controller/form-controller";
import { authHeaders } from "@/auth/server";
import { fromFields } from "@/lib/form-builder";
import { loadManagedEvent } from "../../../data";
import { FormBuilder } from "./form-builder";

export const metadata: Metadata = { title: "Kayıt formu", robots: { index: false } };

export default async function EventFormPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    const [{ tickets }, form] = await Promise.all([
        loadManagedEvent(id),
        getForm(id, { headers: await authHeaders() }).then((r) => r.data),
    ]);

    return (
        <>
            <p className="mb-8 max-w-2xl leading-relaxed text-muted-foreground">
                Kayıt olurken katılımcıya sorulacak sorular. Bir soruyu önceki bir cevaba
                bağlayabilirsin; örneğin &quot;Araba&quot; diyenlere plakayı sor.
            </p>
            <FormBuilder
                key={form.version ?? 0}
                eventId={id}
                initial={fromFields(form.fields ?? [])}
                tickets={tickets}
            />
        </>
    );
}
