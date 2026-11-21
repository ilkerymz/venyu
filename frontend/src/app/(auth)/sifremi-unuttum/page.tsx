import type { Metadata } from "next";
import Link from "next/link";
import { AuthHeading } from "../auth-heading";
import { ForgotPasswordForm } from "./forgot-form";

export const metadata: Metadata = { title: "Şifreni sıfırla" };

export default function ForgotPasswordPage() {
    return (
        <>
            <AuthHeading title="Şifreni sıfırla">
                Hesabının e-posta adresini yaz, yeni şifre belirlemen için bir bağlantı gönderelim.
            </AuthHeading>
            <ForgotPasswordForm />
            <p className="mt-8 text-sm text-muted-foreground">
                <Link
                    href="/giris"
                    className="font-medium text-primary underline-offset-4 hover:underline"
                >
                    Giriş sayfasına dön
                </Link>
            </p>
        </>
    );
}
