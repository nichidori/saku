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
            PickerIcon("Washing Machine", Lucide.WashingMachine),
            PickerIcon("Fan", Lucide.Fan),
            PickerIcon("Heater", Lucide.Heater),
            PickerIcon("Door Open", Lucide.DoorOpen),
            PickerIcon("Key", Lucide.Key),
            PickerIcon("Shower Head", Lucide.ShowerHead),
            PickerIcon("Warehouse", Lucide.Warehouse),
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
            PickerIcon("Wine", Lucide.Wine),
            PickerIcon("Donut", Lucide.Donut),
            PickerIcon("Beef", Lucide.Beef),
            PickerIcon("Candy", Lucide.Candy),
            PickerIcon("Egg", Lucide.Egg),
            PickerIcon("Hop", Lucide.Hop),
            PickerIcon("Ice Cream Cone", Lucide.IceCreamCone),
            PickerIcon("Milk", Lucide.Milk),
            PickerIcon("Apple", Lucide.Apple),
            PickerIcon("Banana", Lucide.Banana),
            PickerIcon("Cherry", Lucide.Cherry),
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
            PickerIcon("Wallet", Lucide.Wallet),
            PickerIcon("Scan", Lucide.Scan),
            PickerIcon("Footprints", Lucide.Footprints),
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
            PickerIcon("Cigarette", Lucide.Cigarette),
            PickerIcon("Cigarette Off", Lucide.CigaretteOff),
            PickerIcon("Dna", Lucide.Dna),
            PickerIcon("Bandage", Lucide.Bandage),
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
            PickerIcon("Chart Pie", Lucide.ChartPie),
            PickerIcon("Trending Up", Lucide.TrendingUp),
            PickerIcon("Landmark", Lucide.Landmark),
            PickerIcon("Calculator", Lucide.Calculator),
            PickerIcon("Scale", Lucide.Scale),
            PickerIcon("Hand Coins", Lucide.HandCoins),
            PickerIcon("Wallet", Lucide.Wallet),
            PickerIcon("Euro", Lucide.Euro),
            PickerIcon("Pound", Lucide.PoundSterling),
            PickerIcon("Yen", Lucide.JapaneseYen),
            PickerIcon("Vault", Lucide.Vault),
            PickerIcon("Gavel", Lucide.Gavel),
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
            PickerIcon("Tram", Lucide.TramFront),
            PickerIcon("Cable Car", Lucide.CableCar),
            PickerIcon("Car Front", Lucide.CarFront),
            PickerIcon("Anchor", Lucide.Anchor),
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
            PickerIcon("Languages", Lucide.Languages),
            PickerIcon("Telescope", Lucide.Telescope),
            PickerIcon("Notebook Pen", Lucide.NotebookPen),
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
            PickerIcon("Clapperboard", Lucide.Clapperboard),
            PickerIcon("Mic", Lucide.Mic),
            PickerIcon("Speaker", Lucide.Speaker),
            PickerIcon("Cast", Lucide.Cast),
            PickerIcon("Drum", Lucide.Drum),
            PickerIcon("Piano", Lucide.Piano),
            PickerIcon("Joystick", Lucide.Joystick),
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
            PickerIcon("Wind", Lucide.Wind),
            PickerIcon("Rain", Lucide.CloudRain),
            PickerIcon("Lightning", Lucide.CloudLightning),
            PickerIcon("Cloud Sun", Lucide.CloudSun),
            PickerIcon("Moon", Lucide.Moon),
            PickerIcon("Sunrise", Lucide.Sunrise),
            PickerIcon("Sunset", Lucide.Sunset),
            PickerIcon("Fish", Lucide.Fish),
            PickerIcon("Rabbit", Lucide.Rabbit),
            PickerIcon("Squirrel", Lucide.Squirrel),
            PickerIcon("Trees", Lucide.Trees),
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
            PickerIcon("User Round", Lucide.UserRound),
            PickerIcon("User Plus", Lucide.UserRoundPlus),
            PickerIcon("User Check", Lucide.UserRoundCheck),
            PickerIcon("User Minus", Lucide.UserRoundMinus),
            PickerIcon("User X", Lucide.UserRoundX),
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
            PickerIcon("Hard Drive", Lucide.HardDrive),
            PickerIcon("Keyboard", Lucide.Keyboard),
            PickerIcon("Mouse", Lucide.Mouse),
            PickerIcon("Printer", Lucide.Printer),
            PickerIcon("Server", Lucide.Server),
            PickerIcon("Webcam", Lucide.Webcam),
            PickerIcon("Battery", Lucide.BatteryFull),
            PickerIcon("Bluetooth", Lucide.Bluetooth),
        )
    ),

    IconCategory(
        "Misc", listOf(
            PickerIcon("Star", Lucide.Star),
            PickerIcon("Bookmark", Lucide.Bookmark),
            PickerIcon("Flag", Lucide.Flag),
            PickerIcon("Bell", Lucide.Bell),
            PickerIcon("Lock", Lucide.Lock),
            PickerIcon("Lock Open", Lucide.LockOpen),
            PickerIcon("Search", Lucide.Search),
            PickerIcon("Map Pin", Lucide.MapPin),
            PickerIcon("Calendar", Lucide.Calendar),
            PickerIcon("Clock", Lucide.Clock),
            PickerIcon("Sparkles", Lucide.Sparkles),
            PickerIcon("Crown", Lucide.Crown),
            PickerIcon("Rocket", Lucide.Rocket),
            PickerIcon("Trash", Lucide.Trash2),
            PickerIcon("Archive", Lucide.Archive),
            PickerIcon("Gem", Lucide.Gem),
            PickerIcon("Magnet", Lucide.Magnet),
            PickerIcon("Plug", Lucide.Plug),
            PickerIcon("Scissors", Lucide.Scissors),
            PickerIcon("Hammer", Lucide.Hammer),
            PickerIcon("Wrench", Lucide.Wrench),
        )
    ),

    IconCategory(
        "Clothing", listOf(
            PickerIcon("Shirt", Lucide.Shirt),
            PickerIcon("Watch", Lucide.Watch),
            PickerIcon("Glasses", Lucide.Glasses),
            PickerIcon("Footprints", Lucide.Footprints),
        )
    ),

    IconCategory(
        "Travel", listOf(
            PickerIcon("Map", Lucide.Map),
            PickerIcon("Compass", Lucide.Compass),
            PickerIcon("Luggage", Lucide.Luggage),
            PickerIcon("Hotel", Lucide.Hotel),
            PickerIcon("Globe", Lucide.Globe),
            PickerIcon("Plane", Lucide.Plane),
        )
    ),
)

/** Flat list for search/filtering across all categories */
val AllPickerIcons: List<PickerIcon> = IconPickerCategories.flatMap { it.icons }

fun String?.toPickerIcon(): PickerIcon? {
    return AllPickerIcons.find { it.label == this }
}