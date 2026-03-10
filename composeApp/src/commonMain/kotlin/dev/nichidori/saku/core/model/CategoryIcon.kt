package dev.nichidori.saku.core.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.*

data class PickerIcon(val label: String, val icon: ImageVector)
data class IconCategory(val name: String, val icons: List<PickerIcon>)

val IconPickerCategories: List<IconCategory> = listOf(

    IconCategory(
        "Home & Living", listOf(
            PickerIcon("Home", Lucide.House),
            PickerIcon("Sofa", Lucide.Sofa),
            PickerIcon("Bed", Lucide.BedDouble),
            PickerIcon("Bath", Lucide.Bath),
            PickerIcon("Lamp", Lucide.Lamp),
            PickerIcon("Lightbulb", Lucide.Lightbulb),
            PickerIcon("Refrigerator", Lucide.Refrigerator),
            PickerIcon("Microwave", Lucide.Microwave),
            PickerIcon("Armchair", Lucide.Armchair),
            PickerIcon("Blinds", Lucide.Blinds),
        )
    ),

    IconCategory(
        "Food & Drink", listOf(
            PickerIcon("Utensils", Lucide.Utensils),
            PickerIcon("Coffee", Lucide.Coffee),
            PickerIcon("Beer", Lucide.Beer),
            PickerIcon("Cake", Lucide.Cake),
            PickerIcon("Pizza", Lucide.Pizza),
            PickerIcon("Sandwich", Lucide.Sandwich),
            PickerIcon("Salad", Lucide.Salad),
            PickerIcon("Soup", Lucide.Soup),
            PickerIcon("Croissant", Lucide.Croissant),
            PickerIcon("Cooking Pot", Lucide.CookingPot),
            PickerIcon("Chef Hat", Lucide.ChefHat),
            PickerIcon("Martini", Lucide.Martini),
        )
    ),

    IconCategory(
        "Shopping", listOf(
            PickerIcon("Shopping Cart", Lucide.ShoppingCart),
            PickerIcon("Shopping Bag", Lucide.ShoppingBag),
            PickerIcon("Shopping Basket", Lucide.ShoppingBasket),
            PickerIcon("Store", Lucide.Store),
            PickerIcon("Receipt", Lucide.Receipt),
            PickerIcon("Tag", Lucide.Tag),
            PickerIcon("Tags", Lucide.Tags),
            PickerIcon("Gift", Lucide.Gift),
            PickerIcon("Ticket", Lucide.Ticket),
            PickerIcon("Package", Lucide.Package),
        )
    ),

    IconCategory(
        "Health & Fitness", listOf(
            PickerIcon("Dumbbell", Lucide.Dumbbell),
            PickerIcon("Heart", Lucide.Heart),
            PickerIcon("Heart Pulse", Lucide.HeartPulse),
            PickerIcon("Activity", Lucide.Activity),
            PickerIcon("Stethoscope", Lucide.Stethoscope),
            PickerIcon("Pill", Lucide.Pill),
            PickerIcon("Syringe", Lucide.Syringe),
            PickerIcon("Hospital", Lucide.Hospital),
            PickerIcon("Microscope", Lucide.Microscope),
            PickerIcon("Biceps Flexed", Lucide.BicepsFlexed),
        )
    ),

    IconCategory(
        "Work & Finance", listOf(
            PickerIcon("Briefcase", Lucide.Briefcase),
            PickerIcon("Building", Lucide.Building2),
            PickerIcon("Dollar Sign", Lucide.DollarSign),
            PickerIcon("Credit Card", Lucide.CreditCard),
            PickerIcon("Banknote", Lucide.Banknote),
            PickerIcon("Piggy Bank", Lucide.PiggyBank),
            PickerIcon("Coins", Lucide.Coins),
            PickerIcon("Chart Bar", Lucide.ChartBar),
            PickerIcon("Trending Up", Lucide.TrendingUp),
            PickerIcon("Landmark", Lucide.Landmark),
            PickerIcon("Calculator", Lucide.Calculator),
            PickerIcon("Scale", Lucide.Scale),
        )
    ),

    IconCategory(
        "Transport", listOf(
            PickerIcon("Car", Lucide.Car),
            PickerIcon("Bus", Lucide.Bus),
            PickerIcon("Train", Lucide.TrainFront),
            PickerIcon("Plane", Lucide.Plane),
            PickerIcon("Bike", Lucide.Bike),
            PickerIcon("Truck", Lucide.Truck),
            PickerIcon("Ship", Lucide.Ship),
            PickerIcon("Taxi", Lucide.CarTaxiFront),
            PickerIcon("Fuel", Lucide.Fuel),
            PickerIcon("Navigation", Lucide.Navigation),
        )
    ),

    IconCategory(
        "Education", listOf(
            PickerIcon("School", Lucide.School),
            PickerIcon("Graduation Cap", Lucide.GraduationCap),
            PickerIcon("Book", Lucide.Book),
            PickerIcon("Book Open", Lucide.BookOpen),
            PickerIcon("Library", Lucide.Library),
            PickerIcon("Pencil", Lucide.Pencil),
            PickerIcon("Notebook", Lucide.Notebook),
            PickerIcon("Atom", Lucide.Atom),
            PickerIcon("Brain", Lucide.Brain),
            PickerIcon("Flask", Lucide.FlaskConical),
        )
    ),

    IconCategory(
        "Entertainment", listOf(
            PickerIcon("Film", Lucide.Film),
            PickerIcon("Music", Lucide.Music),
            PickerIcon("Headphones", Lucide.Headphones),
            PickerIcon("TV", Lucide.Tv),
            PickerIcon("Gamepad", Lucide.Gamepad2),
            PickerIcon("Camera", Lucide.Camera),
            PickerIcon("Theater", Lucide.Theater),
            PickerIcon("Palette", Lucide.Palette),
            PickerIcon("Dice", Lucide.Dices),
            PickerIcon("Popcorn", Lucide.Popcorn),
            PickerIcon("Trophy", Lucide.Trophy),
            PickerIcon("Guitar", Lucide.Guitar),
        )
    ),

    IconCategory(
        "Nature & Outdoors", listOf(
            PickerIcon("Tree", Lucide.TreeDeciduous),
            PickerIcon("Leaf", Lucide.Leaf),
            PickerIcon("Flower", Lucide.Flower),
            PickerIcon("Mountain", Lucide.Mountain),
            PickerIcon("Sun", Lucide.Sun),
            PickerIcon("Cloud", Lucide.Cloud),
            PickerIcon("Umbrella", Lucide.Umbrella),
            PickerIcon("Snowflake", Lucide.Snowflake),
            PickerIcon("Flame", Lucide.Flame),
            PickerIcon("Droplets", Lucide.Droplets),
            PickerIcon("Paw Print", Lucide.PawPrint),
            PickerIcon("Bird", Lucide.Bird),
        )
    ),

    IconCategory(
        "People & Social", listOf(
            PickerIcon("User", Lucide.User),
            PickerIcon("Users", Lucide.Users),
            PickerIcon("Baby", Lucide.Baby),
            PickerIcon("Handshake", Lucide.Handshake),
            PickerIcon("Hand Heart", Lucide.HandHeart),
            PickerIcon("Message Circle", Lucide.MessageCircle),
            PickerIcon("Party Popper", Lucide.PartyPopper),
            PickerIcon("Contact", Lucide.Contact),
            PickerIcon("Phone", Lucide.Phone),
            PickerIcon("Mail", Lucide.Mail),
        )
    ),

    IconCategory(
        "Tech", listOf(
            PickerIcon("Smartphone", Lucide.Smartphone),
            PickerIcon("Laptop", Lucide.Laptop),
            PickerIcon("Monitor", Lucide.Monitor),
            PickerIcon("Tablet", Lucide.Tablet),
            PickerIcon("Wifi", Lucide.Wifi),
            PickerIcon("Shield", Lucide.Shield),
            PickerIcon("Settings", Lucide.Settings),
            PickerIcon("Code", Lucide.Code),
            PickerIcon("Cpu", Lucide.Cpu),
            PickerIcon("Database", Lucide.Database),
            PickerIcon("Globe", Lucide.Globe),
            PickerIcon("Router", Lucide.Router),
        )
    ),

    IconCategory(
        "Misc", listOf(
            PickerIcon("Star", Lucide.Star),
            PickerIcon("Bookmark", Lucide.Bookmark),
            PickerIcon("Flag", Lucide.Flag),
            PickerIcon("Bell", Lucide.Bell),
            PickerIcon("Lock", Lucide.Lock),
            PickerIcon("Search", Lucide.Search),
            PickerIcon("Map Pin", Lucide.MapPin),
            PickerIcon("Calendar", Lucide.Calendar),
            PickerIcon("Clock", Lucide.Clock),
            PickerIcon("Sparkles", Lucide.Sparkles),
            PickerIcon("Crown", Lucide.Crown),
            PickerIcon("Rocket", Lucide.Rocket),
        )
    ),
)

/** Flat list for search/filtering across all categories */
val AllPickerIcons: List<PickerIcon> = IconPickerCategories.flatMap { it.icons }

fun String?.toCategoryIcon(): ImageVector? {
    return AllPickerIcons.find { it.label == this }?.icon
}