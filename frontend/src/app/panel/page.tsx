import { redirect } from "next/navigation";

/** Organizatörün etkinlikleri ve rezervasyonları Hesabım'da (FE-11); eski bağlantılar oraya düşer. */
export default function PanelPage() {
    redirect("/hesabim");
}
