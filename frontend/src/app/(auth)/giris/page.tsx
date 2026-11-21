import type { Metadata } from "next";
import { Suspense } from "react";
import Link from "next/link";
import { Notice } from "@/components/form-parts";
import { FormSkeleton } from "../form-skeleton";
import { AuthHeading } from "../auth-heading";
import { LoginForm } from "./login-form";

export const metadata: Metadata = { title: "Giriş yap" };

type Props = { searchParams: Promise<{ next?: string; sifre?: string }> };

/**
 * Başlık statik; URL'e (next, sifre) bağlı kısım Suspense içinde. Böylece sayfaya geçiş anında
 * başlar (Next.js 16 instant navigation), parametreler gelince form dolar.
 */
export default function LoginPage({ searchParams }: Props) {
    return (
        <>
            <AuthHeading title="Giriş yap">Etkinliklerine ve biletlerine ulaş.</AuthHeading>
            <Suspense fallback={<FormSkeleton />}>
                <LoginBody searchParams={searchParams} />
            </Suspense>
        </>
    );
}

async function LoginBody({ searchParams }: Props) {
    const { next, sifre } = await searchParams;
    const registerHref = next ? `/kayit?next=${encodeURIComponent(next)}` : "/kayit";
    return (
        <>
            {sifre === "yenilendi" && (
                <div className="mb-6">
                    <Notice>Şifren yenilendi. Yeni şifrenle giriş yapabilirsin.</Notice>
                </div>
            )}
            <LoginForm next={next} />
            <p className="mt-8 text-sm text-muted-foreground">
                Hesabın yok mu?{" "}
                <Link
                    href={registerHref}
                    className="font-medium text-primary underline-offset-4 hover:underline"
                >
                    Kayıt ol
                </Link>
            </p>
        </>
    );
}
