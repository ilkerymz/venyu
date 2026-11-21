import { HeaderBar } from "@/components/header-bar";

/** Giriş, kayıt ve e-posta bağlantılarının açtığı sayfalar: dar, tek sütun, dikkat dağıtmayan. */
export default function AuthLayout({ children }: { children: React.ReactNode }) {
    return (
        <>
            <HeaderBar />
            <main className="mx-auto w-full max-w-md px-5 pt-10 pb-20 sm:pt-16">{children}</main>
        </>
    );
}
