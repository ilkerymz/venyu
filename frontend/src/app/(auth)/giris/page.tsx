import type { Metadata } from "next";
import Link from "next/link";
import { Notice } from "@/components/form-parts";
import { AuthHeading } from "../auth-heading";
import { LoginForm } from "./login-form";

export const metadata: Metadata = { title: "Giriş yap" };

type Props = { searchParams: Promise<{ next?: string; sifre?: string }> };

export default async function LoginPage({ searchParams }: Props) {
    const { next, sifre } = await searchParams;
    const registerHref = next ? `/kayit?next=${encodeURIComponent(next)}` : "/kayit";
    return (
        <>
            <AuthHeading title="Giriş yap">Etkinliklerine ve biletlerine ulaş.</AuthHeading>
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
