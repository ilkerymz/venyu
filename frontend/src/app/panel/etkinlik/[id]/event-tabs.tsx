import { SectionTabs } from "@/components/section-tabs";

const TABS = [
    { href: "", label: "Genel" },
    { href: "/katilimcilar", label: "Katılımcılar" },
    { href: "/davetler", label: "Davetler" },
    { href: "/form", label: "Kayıt formu" },
    { href: "/kapi", label: "Kapı" },
];

export function EventTabs() {
    return <SectionTabs prefix="/panel/etkinlik" tabs={TABS} label="Etkinlik yönetimi" />;
}
