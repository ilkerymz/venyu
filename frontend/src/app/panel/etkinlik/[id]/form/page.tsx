import type { Metadata } from "next";
import Link from "next/link";
import { get2 as getForm } from "@/api/generated/form-controller/form-controller";
import { authHeaders } from "@/auth/server";
import { SiteHeader } from "@/components/site-header";
import { fromFields } from "@/lib/form-builder";
import { loadManagedEvent } from "../../../data";
import { FormBuilder } from "./form-builder";

export const metadata: Metadata = { title: "Kayıt formu", robots: { index: false } };

export default async function EventFormPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    const [{ event, tickets }, form] = await Promise.all([
        loadManagedEvent(id),
        getForm(id, { headers: await authHeaders() }).then((r) => r.data),
    ]);

    return (
        <>
            <SiteHeader />
            <main className="mx-auto max-w-6xl px-5 pt-6 pb-20 sm:px-8">
                <Link
                    href={`/panel/etkinlik/${id}`}
                    className="text-sm text-muted-foreground underline-offset-4 hover:underline"
                >
                    {event.title}
                </Link>
                <h1 className="mt-2 text-4xl font-bold tracking-[-0.02em] [font-stretch:85%] sm:text-5xl">
                    Kayıt formu
                </h1>
                <p className="mt-3 mb-8 max-w-2xl leading-relaxed text-muted-foreground">
                    Kayıt olurken katılımcıya sorulacak sorular. Bir soruyu önceki bir cevaba
                    bağlayabilirsin; örneğin &quot;Araba&quot; diyenlere plakayı sor.
                </p>
                <FormBuilder
                    key={form.version ?? 0}
                    eventId={id}
                    initial={fromFields(form.fields ?? [])}
                    tickets={tickets}
                />
            </main>
        </>
    );
}
