import json
import random
import string
import math
from geopy.distance import geodesic

# Center of London
london_center_lat = 51.5074
london_center_lon = -0.1278


def generate_random_geolocation(center_lat, center_lon, radius=25):
    # Generate random distance and angle
    random_dist = random.uniform(0, radius)
    random_angle = random.uniform(0, 360)

    # Calculate new latitude and longitude
    new_lat = center_lat + (random_dist / 111) * math.cos(math.radians(random_angle))
    new_lon = center_lon + (random_dist / 111) * math.sin(math.radians(random_angle)) / math.cos(math.radians(center_lat))

    return round(new_lat, 6), round(new_lon, 6)


manufacturers_models = {
    "Toyota": ["Camry", "Corolla", "Prius", "Avalon", "Highlander", "RAV4"],
    "Ford": ["Fiesta", "Focus", "Mustang", "Escape", "Explorer", "F-150"],
    "Honda": ["Civic", "Accord", "CR-V", "Fit", "Pilot", "Odyssey"],
    "Chevrolet": ["Malibu", "Impala", "Camaro", "Tahoe", "Suburban", "Silverado"],
    "BMW": ["3 Series", "5 Series", "X5", "X3", "7 Series", "M3"],
    "Mercedes-Benz": ["C-Class", "E-Class", "GLA", "GLC", "GLE", "S-Class"],
    "Audi": ["A3", "A4", "A6", "Q5", "Q7", "Q3"],
    "Nissan": ["Altima", "Sentra", "Maxima", "Rogue", "Murano", "Pathfinder"],
    "Hyundai": ["Elantra", "Sonata", "Tucson", "Santa Fe", "Kona", "Palisade"],
    "Volkswagen": ["Jetta", "Passat", "Tiguan", "Golf", "Atlas", "Beetle"],
    "Kia": ["Optima", "Soul", "Sorento", "Sportage", "Stinger", "Telluride"],
    "Subaru": ["Impreza", "Legacy", "Outback", "Forester", "Crosstrek", "Ascent"],
    "Fiat": ["500", "Panda", "Tipo", "Punto", "Doblo", "124 Spider"],
    "Alfa Romeo": ["Giulia", "Stelvio", "Giulietta", "4C", "MiTo", "GTV"]
}


colours = ["Red", "Blue", "Green", "Black", "White", "Grey", "Brown", "Pink"]


owners = [
    "John Smith", "Jane Doe", "Alice Johnson", "Chris Evans", "Patricia Brown",
    "Michael Wilson", "Linda Davis", "Robert Garcia", "Susan Miller", "David Martinez",
    "James Rodriguez", "Jennifer Lee", "Daniel Taylor", "Sarah Lewis", "Paul Clark",
    "Laura Allen", "Thomas Young", "Karen Walker", "Mark Hall", "Emily Hernandez",
    "Charles King", "Jessica Wright", "Matthew Lopez", "Amy Scott", "Steven Hill",
    "Angela Green", "Andrew Adams", "Melissa Baker", "Joshua Nelson", "Rebecca Mitchell",
    "Jason Roberts", "Michelle Perez", "Kevin Turner", "Lisa Campbell", "Brian Edwards",
    "Samantha Carter", "Gary Phillips", "Deborah Parker", "Eric Morris", "Laura Peterson",
    "Ryan Cooper", "Ashley Reed", "Jacob Bailey", "Emily Powell", "Justin Foster",
    "Sharon Rivera", "Brandon Howard", "Brittany Brooks", "Adam Ward", "Emma Torres"
]

data_points = []


for i in range(100):
    manufacturer = random.choice(list(manufacturers_models.keys()))
    model = random.choice(manufacturers_models[manufacturer])
    colour = random.choice(colours)
    owner = random.choice(owners)
    lat, lon = generate_random_geolocation(london_center_lat, london_center_lon)
    identifier = ''.join(random.choices(string.ascii_uppercase + string.digits, k=6))
    label = f"Car-{manufacturer}-{model}-{identifier}"
    comment = f"A car digital twin labelled Car-{manufacturer}-{model}-{identifier}"

    data_point = {
        "unit": random.randint(10, 99),
        "comment": comment,
        "label": label,
        "manufacturerName": manufacturer,
        "colour": colour,
        "model": model,
        "identifier": identifier,
        "owner": owner,
        "isOperational": random.choice([True, False]),
        "location": {
            "latitude": lat,
            "longitude": lon
        }
    }
    data_points.append(data_point)


print(json.dumps(data_points, indent=4))
