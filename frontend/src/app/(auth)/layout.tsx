import { Wordmark } from "@/components/wordmark";

/** Giriş, kayıt ve e-posta bağlantılarının açtığı sayfalar: dar, tek sütun, dikkat dağıtmayan. */
export default function AuthLayout({ children }: { children: React.ReactNode }) {
    return (
        <>
            <header className="mx-auto max-w-6xl px-5 py-5 sm:px-8">
                <Wordmark />
            </header>
            <main className="mx-auto w-full max-w-md px-5 pt-10 pb-20 sm:pt-16">{children}</main>
        </>
    );
}
