import type { Metadata } from "next";
import Link from "next/link";
import { AuthHeading } from "../auth-heading";
import { RegisterForm } from "./register-form";

export const metadata: Metadata = { title: "Hesap oluştur" };

export default function RegisterPage() {
    return (
        <>
            <AuthHeading title="Hesap oluştur">
                Etkinliklere katıl, biletlerini tek yerde tut, kendi etkinliğini düzenle.
            </AuthHeading>
            <RegisterForm />
            <p className="mt-8 text-sm text-muted-foreground">
                Zaten hesabın var mı?{" "}
                <Link
                    href="/giris"
                    className="font-medium text-primary underline-offset-4 hover:underline"
                >
                    Giriş yap
                </Link>
            </p>
        </>
    );
}
